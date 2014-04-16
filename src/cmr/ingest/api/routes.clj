(ns cmr.ingest.api.routes
  "Defines the HTTP URL routes for the application."
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [clojure.string :as string]
            [ring.util.response :as r]
            [ring.util.codec :as codec]
            [ring.middleware.json :as ring-json]
            [clojure.stacktrace :refer [print-stack-trace]]
            [cheshire.core :as cheshire]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.api.errors :as errors]
            [cmr.ingest.services.ingest :as ingest]
            [cmr.system-trace.http :as http-trace]))

(defn- set-concept-id
  "Set concept-id in concept if it is passed in the header"
  [concept headers]
  (let [concept-id (get headers "concept-id")]
    (if (empty? concept-id)
      concept
      (assoc concept :concept-id concept-id))))

(defn- build-routes [system]
  (routes
    (context "/providers/:provider-id" [provider-id]
      (context "/collections/:native-id" [native-id]
        (PUT "/" {:keys [body content-type headers request-context]}
          (let [metadata (string/trim (slurp body))
                base-concept {:metadata metadata
                              :format content-type
                              :provider-id provider-id
                              :native-id native-id
                              :concept-type :collection}
                concept (set-concept-id base-concept headers)]
            (r/response (ingest/save-concept request-context concept))))
        (DELETE "/" {:keys [request-context]}
          (let [concept-attribs {:provider-id provider-id
                                 :native-id native-id
                                 :concept-type :collection}]
            (r/response (ingest/delete-concept request-context concept-attribs)))))
      (context "/granules/:native-id" [native-id]
        (PUT "/" {:keys [body content-type headers request-context]}
          (let [metadata (string/trim (slurp body))
                base-concept {:metadata metadata
                              :format content-type
                              :provider-id provider-id
                              :native-id native-id
                              :concept-type :granule}
                concept (set-concept-id base-concept headers)]
            (r/response (ingest/save-concept request-context concept))))
        (DELETE "/" {:keys [request-context]}
          (let [concept-attribs {:provider-id provider-id
                                 :native-id native-id
                                 :concept-type :granule}]
            (r/response (ingest/delete-concept request-context concept-attribs))))))
    (route/not-found "Not Found")))

(defn make-api [system]
  (-> (build-routes system)
      (http-trace/build-request-context-handler system)
      errors/exception-handler
      handler/site
      ring-json/wrap-json-body
      ring-json/wrap-json-response))


