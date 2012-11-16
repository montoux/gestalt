# gestalt

Gestalt is a small library that does one thing: it moves configuration parameters such as database connection parameters or SSL keystore locations out of your Clojure code and into a configuration file that lives outside your source tree.

Use gestalt to define different environments with different configuration (development, test, staging, production, etc).

## Installation
Gestalt is available as a .jar file on [Clojars.org](https://clojars.org/montoux/gestalt). Include Gestalt in your project using leiningen or maven.

### Leiningen
Add this to your `:dependencies` in `project.clj`:

    [montoux/gestalt "2.0.3"]

### Maven
Add this to your maven pom:

    <dependency>
      <groupId>montoux</groupId>
      <artifactId>gestalt</artifactId>
      <version>2.0.0</version>
    </dependency>
    

## Usage

    (ns my-app
      (:require [montoux.gestalt :as gestalt]))

    ;; if you defined :db-name
    (gestalt/get :db-name)
    ;; => "my_database"

    (gestalt/environment)
    ;; => :development

## Differences with other libraries

A similar open source Clojure configuration tool is [Milieu](https://github.com/drakerlabs/milieu). Though similar in functionality, there are some differences:

 * Gestalt uses standard Clojure for its configuration file syntax; Milieu uses YAML
 * Gestalt encourages you to keep your configuration files outside of your source tree, to avoid accidentally leaking sensitive information (such as database usernames and passwords) by including your configuration file in version control.
 * Gestalt throws an exception when the configuration file could not be read, or when you refer to a key that is not set in the configuration file. Milieu logs a warning.
 * Gestalt has less dependencies and is simpler; Milieu has a bit more functionality, such as overriding configuration values on the commandline.

Gestalt was developed to provide a simple configuration tool that fails early in case there is an error. Milieu is more forgiving. Use the tool that suits your needs. :)



## Public API

### `with-config`
 * `(with-config cfg & body)`

temporarily binds configuration to the map `cfg` and evaluates `body`. Useful for testing.

### `with-environment`
 * `(with-environment env & body)`

temporarily sets the environment to `env` and evaluates `body`. Useful for testing.

### `with-scoped-config`
 * `(with-scoped-config & body)`

Creates a temporary scope for `reset-gestalt!` and evaluates `body`. Useful for testing.

### `reset-gestalt!`
 * `(reset-gestalt!)`
 * `(reset-gestalt! file-or-reader)`
 * `(reset-gestalt! file-or-reader env)`

Initialises the configuration system by reading the configuration file. If the system is not initialised before a call to `get` or `environment`, this function is called automatically.

### `defined?`
 * `(defined? k)`

test if key `k` is defined in the configuration

### `get`
 * `(get & ks)`

gets a config value for the current environment, e.g `(gestalt/get :db :host)`.

### `environment`
 * `(environment)`

returns the environment the application is running in.

## Configuration file

The configuration file should be a clojure nested map, where the
top-level keys are keywords denoting environments, such
as `:development`, `:test`, `:staging` or `:production`. Values are arbitrary (and possibly nested)
maps.

For example:

    {:development {:db "sqlite"
                   :db-host "127.0.0.1"
                   :db-name "devdb"
                   :db-user "foo"
                   :db-pass "..."}
     :testing     {:db "postgresql"
                   :db-host "127.0.0.1"
                   :db-name "testdb"
                   :db-user "testuser"
                   :db-pass "...."}
     :production  {:db "postgresql"
                   :db-host "my.databaseserver.com"
                   :db-name "productiondb"
                   :db-user "productionuser"
                   :db-pass "...."}}



The default location of the config file is `$HOME/.gestalt/config.clj`,
but this can be overridden with the java property
`gestalt.file`, or a file can be specified in the call to `reset-gestalt!`.

*NB: the location of the config file is deliberately kept outside of
version control and the classpath, since we don't encourage storing
passwords in version control, for security reasons.*

Specify the environment to run in with the java property
`gestalt.environment`. The default is `development`. Java property strings specifying an environment are translated to keywords. So "development" refers to the environment `:development` in the configuration file. , The environment can also be specified in the call to `reset-gestalt!`.

The configuration is initialised by calling `reset-gestalt!`, or the
first time `get` or `environment` are used.

## Change History

 * __2012.08.11__ Introduced `with-environment`, `with-config` and `with-scoped-config`; removed `with-cfg-store`. Version 2.0.0.
 * __2012.07.28__ Released version 1.0.0, first public release

## Who?

Gestalt is made by _Gert_ at [Montoux](http://montoux.com) in Wellington, New Zealand.

## License

Copyright © 2012 [Montoux Limited](http://montoux.com)

Distributed under the Eclipse Public License, the same as Clojure.
