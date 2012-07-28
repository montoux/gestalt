# gestalt

Gestalt is a small library that does one thing: it moves configuration parameters such as database connection parameters or SSL keystore locations out of your Clojure code and into a configuration file that lives outside your source tree.

Use gestalt to define different environments with different configuration (development, test, staging, production, etc).

## Usage

    (ns my-app
      (:require [montoux.gestalt :as gestalt]))

    ;; if you defined :db-name
    (gestalt/get :db-name)
    ;; => "my_database"

    (gestalt/environment)
    ;; => :development


## Public API

### `with-cfg-store`
temporarily binds the cfg store to the specified atom and evaluates body. Useful for testing.

### `reset-gestalt!`
initialises the configuration system by reading the configuration file. If the system is not initialised before a call to `get` or `environment`, this function is called automatically.

### `defined?`
test if a key is defined in the configuration

### `get`
gets a config value for the current environment.

### `environment`
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
version control and the classpath, since we don't want to store
passwords in version control - or jar/war files.*

Specify the environment to run in with the java property
`gestalt.environment`. The default is `development`. Java property strings specifying an environment are translated to keywords. So "development" refers to the environment `:development` in the configuration file. , The environment can also be specified in the call to `reset-gestalt!`.

The configuration is initialised by calling `reset-gestalt!`, or the
first time `get` or `environment` are used.

## Change History

 * __2012.07.28__ Released version 1.0.0, first public release

## Who?

Gestalt is made by _Gert_ at [Montoux](http://montoux.com) in Wellington, New Zealand.

## License

Copyright © 2012 [Montoux Limited](http://montoux.com)

Distributed under the Eclipse Public License, the same as Clojure.
