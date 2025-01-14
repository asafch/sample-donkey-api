(ns sample-donkey-api.http.middleware.ip-resolver
  (:require [sample-donkey-api.application.protocols :as protocols]
            [clojure.core.async :as async]))

(defn create-ip-resolver-middleware [ip-resolver]
  (fn [handler]
    (fn [request respond raise]
      (async/go
        (if-let [resolved-ip (async/<! (protocols/resolve-ip ip-resolver (-> request :body-params :ip)))]
          (handler (assoc request :sample/resolved-ip resolved-ip) respond raise)
          (handler request respond raise))))))
