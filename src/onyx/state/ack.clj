(ns ^:no-doc onyx.state.ack
  (:require [taoensso.timbre :refer [info error warn trace fatal] :as timbre]
            [onyx.extensions :as extensions]
            [onyx.types :refer [dec-count! inc-count!]]
            [onyx.static.swap-pair :refer [swap-pair!]]))

(defprotocol AckState 
  (prepare [this id ack-val])
  (defer [this id ack-val])
  (ack [this id ack-val]))

(defrecord StandardAcker [peer-replica-state messenger]
  AckState
  (prepare [this _ ack-val]
    (inc-count! ack-val)
    this)

  (defer [this _ ack-val]
    this)

  (ack [this _ ack-val]
    ; (when (dec-count! ack-val)
    ;   (when-let [site (peer-site peer-replica-state (:completion-id ack-val))]
    ;     (extensions/ack-barrier messenger site ack-val)))
    this))

(defrecord DeduplicationAckState [ack-state peer-replica-state messenger]
  AckState
  (prepare [this id ack-val]
    (let [[old-val new-val] (swap-pair! ack-state
                                        (fn [s] 
                                          (if (s id)
                                            s
                                            (assoc s id (list ack-val)))))]
      (when-not (= old-val new-val)
        (inc-count! ack-val)))
    this)

  (defer [this id ack-val]
    (let [[old-val new-val] (swap-pair! ack-state 
                                        (fn [s]
                                          (if (s id)
                                            (update s id conj ack-val)
                                            s)))]
      (when-not (= old-val new-val)
        (inc-count! ack-val)) 
      this))

  (ack [this id _]
    (let [[old-val new-val] (swap-pair! ack-state (fn [s] (dissoc s id)))]
      ; (when-not (= old-val new-val)
      ;   (run! (fn [ack-val] 
      ;           (when (dec-count! ack-val)
      ;             (when-let [site (peer-site peer-replica-state (:completion-id ack-val))]
      ;               (extensions/ack-barrier messenger site ack-val)))) 
      ;         (old-val id))) 
      this)))

(defn new-ack-state [task-map peer-replica-state messenger]
  (if (contains? task-map :onyx/uniqueness-key)
    (->DeduplicationAckState (atom {}) peer-replica-state messenger)
    (->StandardAcker peer-replica-state messenger)))
