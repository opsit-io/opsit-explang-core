;;; prints sorted word frequence table for a text

(defun freqcount(txt)
  ;; create map of counters
  (let ((m (hashmap)))
    ;; for each  word in text that matches pattern
    (foreach (word  (re-seq (re-pattern "\\w+") txt))
	     ;;; lowercase word
	     (let ((lcword (lowercase word)))
	       ;; increment word counter
	       ;; (null is automatically treated as numeric 0)
               (assoc! m lcword (+ 1 (get m lcword)))))
    ;; sort list of Map Entries by their values
    (sort (lambda (a b)
	    (- (get b "value")
	       (get a "value")))
	  ;;; get Map.Entries into list
	  (append () m))))

(print (freqcount "SHALL I compare thee to a summer's day?
Thou art more lovely and more temperate.
Rough winds do shake the darling buds of May,
And summer's lease hath all too short a date:
Sometime too hot the eye of heaven shines,
And often is his gold complexion dimm'd;
And every fair from fair sometimes declines,
By chance, or nature's changing course, untrimm'd;
But thy eternal summer shall not fade
Nor lose possession of that fair thou ow'st;
Nor shall Death brag thou wand'rest in his shade,
When in eternal lines to time thou grow'st.
So long as men can breathe or eyes can see,
So long lives this, and this gives life to thee.") "\n") 
