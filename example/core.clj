(ns example.core
  (:require [exampler.render :as render]))

(render/process-file "test/exampler/sample.clj" render/default-settings)
