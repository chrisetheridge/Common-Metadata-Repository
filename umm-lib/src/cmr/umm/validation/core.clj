(ns cmr.umm.validation.core
  "Defines validations UMM concept types."
  (:require [clj-time.core :as t]
            [cmr.common.validations.core :as v]
            [cmr.umm.validation.utils :as vu]
            [cmr.umm.collection :as c]
            [cmr.umm.granule :as g]
            [cmr.umm.spatial :as umm-s]
            [cmr.spatial.validation :as sv]
            [cmr.umm.related-url-helper :as ruh])
  (:import cmr.umm.collection.UmmCollection
           cmr.umm.granule.UmmGranule))

(defn set-geometries-spatial-representation
  "Sets the spatial represention from the spatial coverage on the geometries"
  [spatial-coverage]
  (let [{:keys [spatial-representation geometries]} spatial-coverage]
    (assoc spatial-coverage
           :geometries
           (map #(umm-s/set-coordinate-system spatial-representation %) geometries))))

(def spatial-coverage-validations
  "Defines spatial coverage validations for collections."
  (v/pre-validation
    ;; The spatial representation has to be set on the geometries before the conversion because
    ;;polygons etc do not know whether they are geodetic or not.
    set-geometries-spatial-representation
    {:geometries (v/every sv/spatial-validation)}))

(def sensor-validations
  "Defines the sensor validations for collections"
  {:characteristics (vu/unique-by-name-validator :name)})

(def instrument-validations
  "Defines the instrument validations for collections"
  {:sensors [(v/every sensor-validations)
             (vu/unique-by-name-validator :short-name)]
   :characteristics (vu/unique-by-name-validator :name)})

(def platform-validations
  "Defines the platform validations for collections"
  {:instruments [(v/every instrument-validations)
                 (vu/unique-by-name-validator :short-name)]
   :characteristics (vu/unique-by-name-validator :name)})

(defn- range-date-time-validation
  "Defines range-date-time validation"
  [field-path value]
  (let [{:keys [beginning-date-time ending-date-time]} value]
    (when (and beginning-date-time ending-date-time (t/after? beginning-date-time ending-date-time))
      {field-path [(format "BeginningDateTime [%s] must be no later than EndingDateTime [%s]"
                           (str beginning-date-time) (str ending-date-time))]})))

(def online-access-urls-validation
  "Defines online access urls validation for collections."
  (v/pre-validation
    ruh/downloadable-urls
    (vu/unique-by-name-validator :url)))

(def collection-validations
  "Defines validations for collections"
  {:product-specific-attributes (vu/unique-by-name-validator :name)
   :projects (vu/unique-by-name-validator :short-name)
   :spatial-coverage spatial-coverage-validations
   :platforms [(v/every platform-validations)
               (vu/unique-by-name-validator :short-name)]
   :associated-difs (vu/unique-by-name-validator identity)
   :temporal {:range-date-times (v/every range-date-time-validation)}
   :related-urls online-access-urls-validation
   :two-d-coordinate-systems (vu/unique-by-name-validator :name)})

(def granule-validations
  "Defines validations for granules"
  {})

(def umm-validations
  "A list of validations by type"
  {UmmCollection collection-validations
   UmmGranule granule-validations})

(defn validate
  "Validates the umm record returning a list of error maps containing a path through the
  UMM model and a list of errors at that path. Returns an empty sequence if it is valid."
  [umm]
  (vu/perform-validation umm (umm-validations (type umm))))


