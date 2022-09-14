;;; The 8 Queens Problem

(setq N 8)

(defun print-solution (a)
  (foreach (i (range 0 N))
	   (foreach  (j  (range 0 N))
		     (print (if (== (get a i) j) #\U2655 "-")))
	   (println))
  (println))

(defun check-placement (a n c)
  "Check if placement (n c) is free from attacks from queens in board a."
  (let ((result true))
    (foreach (i (range 0 n) result)
	     (let ((pos (get a i)))
	       (if (or (= pos c)               ; same column?
		       (= (- pos i) (- c n))   ; same diagonal
		       (= (+ pos i) (+ c n)))  ; same diagonal		     
		   (setv result false))))))

(defun add-queen (a n)
  "Add to board a all queens from n up to N"
  (if (>= n N)
      (print-solution a)         ; all queens have been placed
      (foreach (c (range 0 N))   ; try to place next queen
	       (if (check-placement a n c)
		   (progn 
		     (put! a n c)
		     (add-queen a (+ n 1)))))))

;;(add-queen (make-array :size N :element-type "int") 0)
;;(add-queen '(0 0 0 0 0 0 0 0) 0)
;;(add-queen (hashmap) 0)
(add-queen (make-array :size N ) 0)

