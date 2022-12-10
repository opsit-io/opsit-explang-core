;; for test of LOAD(R), loaded as resource
(STR "INSIDE resloadtest_comperr.l\n")

(==)  ; will cause compilation error (insuff. args)

(SETV *loaded* "some-result") 
