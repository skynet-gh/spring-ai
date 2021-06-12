(ns skynet.server
  (:require
    [compojure.core :refer [defroutes GET POST]]
    [compojure.route :as route]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.session :refer [wrap-session]]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]))



; https://github.com/ptaoussanis/sente#on-the-server-clojure-side

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom


(defroutes my-app-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (route/not-found "<h1>Page not found</h1>"))


(def my-app
  (-> my-app-routes
      wrap-keyword-params
      wrap-params
      wrap-anti-forgery
      wrap-session))
