(ns sample-donkey-api.http.internal.routes
  (:require [reitit.ring :as reitit-ring]
            [reitit.ring.middleware.muuntaja :as reitit-middleware-muuntaja]
            [muuntaja.core :as muuntaja]
            [sample-donkey-api.http.middleware.exception :as exception-middleware]
            [integrant.core :as ig]
            [sample-donkey-api.http.protocols :as protocols]
            [com.appsflyer.donkey.middleware.params :as donkey-params]
            [ring.util.response :as response]))

(defn- get-routes
  [controller]
  ["/_" {:middleware [(donkey-params/parse-query-params {:keywordize true})]}
   ["/liveness" {:get {:handler (partial protocols/liveness controller)}}]
   ["/readiness" {:get {:handler (partial protocols/readiness controller)}}]])

(defn- create-router
  "creates a reitit router and validates its structure"
  [routes]
  (reitit-ring/router
    routes
    {:data {:muuntaja   (-> muuntaja/default-options
                            (update :formats select-keys ["application/json"])
                            muuntaja/create)
            :middleware [exception-middleware/exception-middleware
                         reitit-middleware-muuntaja/format-middleware]}}))

(defn- create-ring-handler [router]
  (reitit-ring/ring-handler
    router
    (reitit-ring/create-default-handler
      {:not-found (constantly (response/status 404))})))

(defn- routes [controller]
  (let [ring-handler (-> controller
                         get-routes
                         create-router
                         create-ring-handler)]
    [{:handler      ring-handler
      :handler-mode :non-blocking}]))

(defmethod ig/init-key :internal/routes [_ {:keys [controller]}]
  (routes controller))
