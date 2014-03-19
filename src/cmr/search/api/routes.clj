(ns cmr.search.api.routes
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [ring.util.response :as r]
            [ring.util.codec :as codec]
            [ring.middleware.json :as ring-json]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.api.errors :as errors]
            [cmr.search.services.query-service :as query-svc]
            [cmr.system-trace.http :as http-trace]
            [cmr.search.api.search-results :as sr]))

(defn get-search-results-format
  "Returns the requested search results format parsed from headers"
  [headers]
  (let [mime-type (get headers "accept")]
    (sr/validate-search-result-mime-type mime-type)
    (sr/mime-type->format mime-type)))

(defn find-collection-references [context params headers]
  (let [result-format (get-search-results-format headers)
        _ (info (format "Search for collections in format [%s] with params [%s]" result-format params))
        results (query-svc/find-concepts-by-parameters context :collection params)]
    {:status 200
     :headers {"Content-Type" (sr/format->mime-type result-format)}
     :body (sr/search-results->response results result-format)}))

(defn- build-routes [system]
  (routes
    (context "/collections" []
      (GET "/" {params :params headers :headers context :request-context}
        (find-collection-references context params headers)))
    (route/not-found "Not Found")))

(defn make-api [system]
  (-> (build-routes system)
      (http-trace/build-request-context-handler system)
      errors/exception-handler
      handler/site
      ring-json/wrap-json-body
      ring-json/wrap-json-response))



