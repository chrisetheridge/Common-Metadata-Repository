(ns cmr.umm-spec.test.serf
  "Tests roundtrip XML generation from a Clojure record and parsing it. Ensures that the same data
  is returned."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.test.check.generators :as gen]
            [clj-time.core :as t]
            [com.gfredericks.test.chuck.clojure-test :refer [for-all]]
            [cmr.common.util :refer [update-in-each]]
            [cmr.common.test.test-check-ext :as ext :refer [defspec]]
            [cmr.umm-spec.test.expected-conversion :as expected-conversion]
            [cmr.umm-spec.test.umm-record-sanitizer :as sanitize]
            [cmr.umm-spec.core :as core]
            [cmr.umm-spec.simple-xpath :refer [select context]]
            [cmr.umm-spec.xml-to-umm-mappings.serf :as serf-xml-to-umm]
            [cmr.umm-spec.json-schema :as js]
            [cmr.umm-spec.models.service :as umm-s]
            [cmr.umm-spec.test.umm-generators :as umm-gen]))

(def tested-formats
  "Seq of formats to use in round-trip conversion and XML validation tests."
  [:serf])

(def example-record
  "An example record with fields supported by most formats."
  (js/coerce js/umm-s-schema
    {:MetadataDates [{:Date "2009-12-03T00:00:00.000Z"
                  :Type "CREATE"},
                 {:Date "2009-12-04T00:00:00.000Z"
                  :Type "UPDATE"}]
    :ServiceLanguage "English"
    :ISOTopicCategories ["CLIMATOLOGY/METEOROLOGY/ATMOSPHERE" 
                         "ENVIRONMENT" 
                         "IMAGERY/BASE MAPS/EARTH COVER"]
    :Abstract "This is one of the GES DISC's OGC Web Coverage Service (WCS) instances which provides Level 3 Gridded atmospheric data products derived from the Atmospheric Infrared Sounder (AIRS) on board NASA's Aqua spacecraft."
    :ServiceCitation [{:Creator "NASA Goddard Earth Sciences (GES) Data and Information Services Center (DISC)"
                        :Title "OGC Web Coverage Service (WCS) for accessing Atmospheric Infrared Sounder (AIRS) Data" }]
    :RelatedUrls [{:Description "\n         This Web Coverage Service (WCS) is one of the multiple GES DISC data service instances used to provide gridded Level 3 Atmospheric Infrared Sounder (AIRS) data products. Accessing to this URL will result in a brief description of coverages (i.e., data layers or variables), or a getCapabilities response. A client can request more detailed information about the served coverages by sending a describeCoverage request to the server. Finally, a client can request actual data using a getCoverage request. \n      "
                  :ContentType {:Type "GET SERVICE" :Subtype "GET WEB COVERAGE SERVICE (WCS)"}
                  :Protocol nil
                  :URLs ["http://acdisc.sci.gsfc.nasa.gov/daac-bin/wcsAIRSL3?Service=WCS&Version=1.0.0&Request=getCapabilities"]
                  :Title nil
                  :MimeType nil
                  :Caption nil}]
      :ServiceKeywords  [{:Category "EARTH SCIENCE SERVICES"
                         :Topic "WEB SERVICES"
                         :Term "DATA APPLICATION SERVICES"}
                        {:Category "EARTH SCIENCE SERVICES"
                         :Topic "WEB SERVICES"
                         :Term "DATA APPLICATION SERVICES"}
                        {:Category "EARTH SCIENCE SERVICES"
                         :Topic "WEB SERVICES"
                         :Term "DATA PROCESSING SERVICES"}
                        {:Category "EARTH SCIENCE SERVICES"
                         :Topic "WEB SERVICES"
                         :Term "INFORMATION MANAGEMENT SERVICES"}]
      :AdditionalAttributes  [{:Group "gov.nasa.gsfc.gcmd",
                                 :Value "2015-11-20 16:04:57",
                                 :Name "metadata.extraction_date"}
                                {:Group "gov.nasa.gsfc.gcmd",
                                 :Value "8.1",
                                 :Name "metadata.keyword_version"}]
      :EntryId "NASA_GES_DISC_AIRS_Atmosphere_Data_Web_Coverage_Service"                               
      :ScienceKeywords [{:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "AEROSOLS"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "AIR QUALITY"}
                      {
                       :Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC CHEMISTRY"}
                      {
                       :Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC TEMPERATURE"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC WATER VAPOR"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC WINDS"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "CLOUDS"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "PRECIPITATION"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC RADIATION"}
                      {:Category "EARTH SCIENCE"
                       :Topic "ATMOSPHERE"
                       :Term "ATMOSPHERIC PRESSURE"}]
       :EntryTitle "OGC Web Coverage Service (WCS) for accessing Atmospheric Infrared Sounder (AIRS) Data"
 :Distributions [{ :DistributionMedia "Digital",
                       :DistributionSize "<=728MB per request",
                       :DistributionFormat "HTTP",
                       :Fees "None"}]   
          :Platforms [{:ShortName "AQUA"
                  :LongName "Earth Observing System, AQUA"
                  :Instruments [{:LongName "Airborne Electromagnetic Profiler"
                 :ShortName "AEM"}
                {:ShortName "AIRS"
                 :LongName "Atmospheric Infrared Sounder"}
                {:ShortName "AERS"
                 :LongName "Atmospheric/Emitted Radiation Sensor"}]}]  
     :Projects  [{ :ShortName "EOS"
                   :LongName "Earth Observing System"}
                  {:ShortName "EOSDIS"
                   :LongName "Earth Observing System Data Information System"}
                  {:ShortName "ESIP"
                   :LongName "Earth Science Information Partners Program"}
                  {:ShortName "OGC/WCS",
                   :LongName "Open Geospatial Consortium/Web Coverage Service"}]}))

(deftest test-serf-parsing
  (let [serf-xml (slurp (io/resource "example_data/serf.xml"))
        serf-umm (serf-xml-to-umm/serf-xml-to-umm-s serf-xml)]
        (is (= serf-umm example-record))))