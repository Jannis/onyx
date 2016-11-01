(ns onyx.messaging.protocols.subscriber
  (:refer-clojure :exclude [key]))

(defprotocol Subscriber
  (sub-info [this])
  (equiv-meta [this sub-info])
  (key [this])
  (start [this])
  (stop [this])
  (poll-messages! [this])
  (poll-replica! [this])
  (set-replica-version! [this new-replica-version])
  (set-epoch! [this new-epoch])
  (get-recover [this])
  ; (session-id [this])
  ; (set-session-id! [this sess-id])
  (offer-ready-reply! [this])
  (completed? [this])
  (unblock! [this])
  (blocked? [this])
  (offer-heartbeat! [this])
  (poll-heartbeats! [this]))
