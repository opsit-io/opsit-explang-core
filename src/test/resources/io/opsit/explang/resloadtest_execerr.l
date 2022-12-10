;; for test of LOAD(R), loaded as resource
(STR "INSIDE resloadtest_execerr.l\n")

(/ 1 0) ; will cause execution exception

(SETV *loaded* "some-result")
