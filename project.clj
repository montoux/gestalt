(defproject montoux/gestalt "2.0.3"
  :description "Gestalt moves configuration parameters such as database connection parameters or SSL keystore locations out of your Clojure code and into a configuration file that lives outside your source tree. Gestalt also lets you specify configuration for different environments (develop, test, staging, production, etcetera)."

  :min-lein-version "2.0.0"
  
  :url "https://github.com/montoux/gestalt"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  
  :dependencies [[org.clojure/clojure "1.4.0"]])
