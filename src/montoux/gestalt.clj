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
       :doc "Stores the configuration"}
  *cfg* nil)

(def ^{:private true :dynamic true
       :doc "Stores the environment"}
  *env* nil)


;; ----------------------------------------------------------------------------
;; helper functions
;; ----------------------------------------------------------------------------

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

(defn- read-config-file
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

(defn- read-config
  "Returns the configuration for the specified environment"
  [file-or-reader]
  (let [form (read-config-file file-or-reader)]
    (when-not (map? form)
      (throw (RuntimeException.
              (if (instance? java.io.File file-or-reader)
                (str "Config file \"" file-or-reader "\" is not in the "
                     "right format. It should be a Clojure map.")
                (str "Configuration is not in the right format. It should be "
                     "a Clojure map.")))))
    form))


;; ----------------------------------------------------------------------------
;; public API
;; ----------------------------------------------------------------------------

(defmacro with-config
  "Temporarily sets the configuration to cfg and evaluates body"
  [cfg & body]
  `(let [v# ~cfg]
     (binding [*cfg* v#]
       ~@body)))

(defmacro with-environment
  "Temporarily sets the environment to env and evaluates body."
  [env & body]
  `(let [v# ~env]
     (binding [*env* v#]
       ~@body)))

(defmacro with-scoped-config
  "Creates a thread-bound scope for configuration and environment and
  evaluates body. This will cause calls to `reset-gestalt!` to only
  affect the config and environment in the scope of body; useful for
  testing." [& body]
  `(with-config nil
     (with-environment nil
       ~@body)))


(defn reset-gestalt!
  "Initialises the configuration system.
   NB: this is called automatically when you first use call `get` or
   `environment`, but you could call this function to specify a custom
   location of the configuration file, or a custom environment.
   When `reset-gestalt!` is called in the body of a `with-scoped-config`
   call, the reset is only visible within body."
  ([] (reset-gestalt! (get-config-file)))
  ([file-or-reader] (reset-gestalt! file-or-reader (get-environment)))
  ([file-or-reader env]
     {:pre [(keyword? env)]}
     (let [cfg (read-config file-or-reader)]
       (when-not (clojure.core/contains? cfg env)
         (throw (RuntimeException.
                 (str "Config does not contain an entry for "
                      "environment \"" env "\"."))))
       (if (thread-bound? (var *cfg*))
         (set! *cfg* cfg)
         (alter-var-root (var *cfg*) (constantly cfg)))
       (if (thread-bound? (var *env*))
         (set! *env* env)
         (alter-var-root (var *env*) (constantly env)))
       nil)))

(defn environment
  "Returns the environment the application is running in."
  []
  (when (nil? *env*) (reset-gestalt!))
  *env*)

(defn defined?
  "Returns true if and only if key k is defined in the configuration map."
  [k]
  (when (nil? *cfg*) (reset-gestalt!))
  (contains? (clojure.core/get *cfg* *env*) k))

(defn get
  "Returns the configuration value for keys ks."
  [& ks]
  (when (nil? *cfg*) (reset-gestalt!))
  (or (get-in *cfg* (concat [*env*] ks))
      (throw (IllegalArgumentException.
              (str "Configuration \"" (vec ks) "\" not found "
                   "in environment " *env*)))))


