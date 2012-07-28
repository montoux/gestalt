(ns ^{:author "Montoux Limited, <info@montoux.com>"
      :doc "Gestalt is a small library that does one thing: it moves configuration parameters such as database connection parameters or SSL keystore locations out of your Clojure code and into a configuration file that lives outside your source tree."}
  montoux.gestalt
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as jio]))


;; ----------------------------------------------------------------------------
;; constants
;; ----------------------------------------------------------------------------

(def ^{:doc "Name of Java property that determines the environment"}
  GESTALT_ENVIRONMENT_PROP "gestalt.environment")

(def ^{:doc "Name of the Java property to specify another config file"}
  GESTALT_CONFIG_FILE_PROP "gestalt.file")

(def ^{:doc "The default config file. Can be overridden by setting the java property
`gestalt.file`, or by calling `reset-gestalt!` with the appropriate filename"}
  DEFAULT_CONFIG_FILE (jio/file (System/getProperty "user.home")
                                ".gestalt" "config.clj"))

(def ^{:doc "The default environment to use. Defaults to \"development\". This value
and the value of java property `gestalt.environment` are specified as strings, but
in the configuration file your environment keys should be keywords, e.g. `:development`."}
  DEFAULT_ENVIRONMENT "development")



;; ----------------------------------------------------------------------------
;; in-memory store of configuration parameters
;; ----------------------------------------------------------------------------
(def ^{:private true :dynamic true
       :doc "Stores the configuration and the name of the environment"}
  *cfg-map* (atom nil))



;; ----------------------------------------------------------------------------
;; helper functions
;; ----------------------------------------------------------------------------


(defn- read-config
  "Reads file and parses it as a clojure form."
  [file-or-reader]
  (try
    (read-string (slurp file-or-reader :encoding "UTF-8"))
    (catch Exception e
      (throw (RuntimeException.
              (if (instance? java.io.File file-or-reader)
                (str "Fatal: Could not read configuration file \""
                     file-or-reader "\". Create a config file at this "
                     "location or specify an alternative location with "
                     "the system property \"" GESTALT_CONFIG_FILE_PROP "\".")
                (str "Fatal: Could not read configuration."))
              e)))))


(defn- get-config-file
  "Return a java.io.File object for the configuration file."
  []
  (or (jio/file (System/getProperty GESTALT_CONFIG_FILE_PROP))
      DEFAULT_CONFIG_FILE))


(defn- get-environment
  "Returns the environment this application should run in. Defaults to
   :development."
  []
  (keyword (or (System/getProperty GESTALT_ENVIRONMENT_PROP)
               DEFAULT_ENVIRONMENT)))


(defn- get-config-for-environment
  "Returns the configuration for the specified environment"
  [env file-or-reader]
  (let [form (read-config file-or-reader)]
    (when-not (map? form)
      (throw (RuntimeException.
              (if (instance? java.io.File file-or-reader)
                (str "Config file \"" file-or-reader "\" is not in the "
                     "right format. It should be a Clojure map.")
                (str "Configuration is not in the right format. It should be "
                     "a Clojure map.")))))
    (when-not (clojure.core/contains? form env)
      (throw (RuntimeException.
              (str "Config file does not contain an entry for "
                   "environment \"" env "\"."))))
    (clojure.core/get form env)))






;; ----------------------------------------------------------------------------
;; public API
;; ----------------------------------------------------------------------------

(defmacro with-cfg-store
  "Temporarily binds the storage for the configuration to atom `store` and
   executes body. Useful for testing."
  [store & body]
  `(do
     (assert (instance? clojure.lang.Atom ~store)
             "`store` parameter must be an atom.")
     (binding [*cfg-map* ~store]
       ~@body)))

(defn reset-gestalt!
  "Initialises the configuration system.
   NB: this is called automatically when you first use call `get` or
   `environment`, but you could call this function to specify a custom
   location of the configuration file, or a custom environment."
  ([] (reset-gestalt! (get-config-file)))
  ([file-or-reader] (reset-gestalt! file-or-reader (get-environment)))
  ([file-or-reader env]
     {:pre [(keyword? env)]}
     (let [cfg (get-config-for-environment env file-or-reader)]
       (reset! *cfg-map* {:cfg cfg :env env})
       nil)))

(defn defined?
  "Returns true if and only if key k is defined in the configuration map."
  [k]
  (when (nil? @*cfg-map*) (reset-gestalt!))
  (contains? (:cfg @*cfg-map*) k))

(defn get
  "Returns the configuration value for key k. The configuration system
   must be initialised by calling `init!` before you can use get."
  [k & ks]
  (when (nil? @*cfg-map*) (reset-gestalt!))
  (or (get-in @*cfg-map* (concat [:cfg k] ks))
      (throw (IllegalArgumentException.
              (str "Configuration \"" (vec (concat [k] ks)) "\" not found.")))))

(defn environment
  "Returns the environment the application is running in."
  []
  (when (nil? @*cfg-map*) (reset-gestalt!))
  (:env @*cfg-map*))

