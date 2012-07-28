(ns ^{:author "Montoux Limited, <info@montoux.com>"
      :doc "N/A"}
  test.montoux.gestalt
  (:use clojure.test)
  (:require [montoux.gestalt :as gestalt]
            [clojure.java.io :as jio])
  (:import java.io.StringReader))


(deftest test-with-cfg-store
  (let [s  (atom {:cfg {::foo 42} :env :test-env})
        s' (atom {:cfg {::foo 3.14} :env :another-env})]
    (gestalt/with-cfg-store s
      (is (= 42 (gestalt/get ::foo)))
      (is (= :test-env (gestalt/environment))))
    (gestalt/with-cfg-store s'
      (is (= 3.14 (gestalt/get ::foo)))
      (is (= :another-env (gestalt/environment))))))

(deftest test-reset-gestalt!
  (let [s (atom nil)]
    (gestalt/with-cfg-store s
      (gestalt/reset-gestalt!
       (StringReader. "{:test {:foo 1 :bar 2} :test2 {:foo 10 :bar 20}}") :test)
      (is (= 1 (gestalt/get :foo))))))

(defn- tmpfile [contents]
  (let [f (doto (java.io.File/createTempFile "cfg-test" ".clj") .deleteOnExit)]
    (spit f contents)
    f))

(defmacro with-system-property
  "Temporary sets system property k to value v.
   restores the old value before returning.
   NB: this modifies Java system properties and is NOT THREAD SAFE!"
  [k v & body]
  `(let [old-value# (System/getProperty ~k)]
     (try
       (System/setProperty ~k ~v)
       ~@body
       (finally
        (if (nil? old-value#)
          (System/clearProperty ~k)
          (System/setProperty ~k old-value#))))))


(deftest test-automatic-reset-gestalt
  (let [s (atom nil)
        f (tmpfile "{:test {:foo 1 :bar 2} :test2 {:foo 10 :bar 20}}")]
    (try
      (gestalt/with-cfg-store s
        (with-system-property gestalt/GESTALT_CONFIG_FILE_PROP (str f)
          
          (with-system-property gestalt/GESTALT_ENVIRONMENT_PROP "test2"
            (is (= 10 (gestalt/get :foo)) "initialised on first use")
            (is (= :test2 (gestalt/environment))))
          
          (with-system-property gestalt/GESTALT_ENVIRONMENT_PROP "test"
            (is (= 10 (gestalt/get :foo)) "not initialised again")
            (is (= :test2 (gestalt/environment))))))
      
      (finally
       (jio/delete-file f)))))

(deftest test-contains?
  (gestalt/with-cfg-store (atom {:cfg {:foo 42} :env :test})
    (is (gestalt/defined? :foo))
    (is (not (gestalt/defined? :bar)))))

(deftest test-get?
  (gestalt/with-cfg-store (atom {:cfg {:foo 42
                                      :a {:b {:c 1}}} :env :test})
    (is (thrown? IllegalArgumentException (gestalt/get :bar)))
    (is (thrown? IllegalArgumentException (gestalt/get nil)))
    (is (= 42 (gestalt/get :foo)))
    (is (= 1 (gestalt/get :a :b :c)))))