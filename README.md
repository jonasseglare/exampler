# exampler

A tool for extracting code examples and typeset them with syntax highlighting in Latex.

## Usage

First, surround all the code snippets that you want to extract with ```[:begin-example "examplename"]``` and ```[:end-example```, e.g.
```clj
(ns exampler.sample)

[:begin-example "Square"]
;; Implement $s(x) = x^2$
(println "Define square")
(defn square [x] (* x x)) ;; [4]
[:end-example]

[:begin-example "Square-eval"]
(def res (square 3))
(println "The result is" res)
res ;; [:result]
[:end-example]
```
Then, in another file, require the ```exampler.render``` namespace and call the ```process-file``` function providing the filename of the file containing the code examples:
```clj
(render/process-file "test/exampler/sample.clj" render/default-settings)
```

This will output some Latex code in the ```latex/exampler``` subdirectory, such as
```latex
\begin{Verbatim}[commandchars=\\\{\},codes={\catcode`\$=3\catcode`\^=7\catcode`\_=8}]
\PY{c+c1}{;; Implement $s(x) = x^2$}
\PY{p}{(}\PY{n+nb}{println }\PY{l+s}{\PYZdq{}Define square\PYZdq{}}\PY{p}{)}
\PY{p}{(}\PY{k+kd}{defn }\PY{n+nv}{square} \PY{p}{[}\PY{n+nv}{x}\PY{p}{]} \PY{p}{(}\PY{n+nb}{* }\PY{n+nv}{x} \PY{n+nv}{x}\PY{p}{)}\PY{p}{)} 
\end{Verbatim}
```

See the ```example/core.clj``` file for an example.

## Dependencies

You need to install

  * Python3
  * Pygments

for this code to work.

## License

Copyright © 2019 Jonas Östlund

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
