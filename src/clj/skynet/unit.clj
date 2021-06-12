(ns skynet.unit
  (:import
    (clojure.lang Keyword)
    (com.springrts.ai.oo.clb Unit UnitDef)))



(defprotocol DefName
  (def-name [this]))

(extend-protocol DefName
  Unit
  (def-name [this] (def-name (.getDef this)))
  UnitDef
  (def-name [this] (.getName this))
  String
  (def-name [this] this)
  Keyword
  (def-name [this] this))


(defmulti typeof (fn [unit-or-def-or-name] (def-name unit-or-def-or-name)))

(defmethod typeof "armcom" [_defname]
  ::com)

(defmethod typeof "corcom" [_defname]
  ::com)

(defmethod typeof "armmex" [_defname]
  ::mex)

(defmethod typeof "cormex" [_defname]
  ::mex)

(defmethod typeof "armsolar" [_defname]
  ::solar)

(defmethod typeof "corsolar" [_defname]
  ::solar)

(defmethod typeof "armlab" [_defname]
  ::kbot-lab)

(defmethod typeof "corlab" [_defname]
  ::kbot-lab)

(defmethod typeof "armrad" [_defname]
  ::radar)

(defmethod typeof "corrad" [_defname]
  ::radar)

(defmethod typeof "armllt" [_defname]
  ::llt)

(defmethod typeof "corllt" [_defname]
  ::llt)

(defmethod typeof "armhlt" [_defname]
  ::hlt)

(defmethod typeof "corhlt" [_defname]
  ::hlt)

(defmethod typeof "armguard" [_defname]
  ::coastal)

(defmethod typeof "corpun" [_defname]
  ::coastal)

(defmethod typeof :default [_defname]
  nil)


(defmulti nameof (fn [side kw] [side kw]))


(defmethod nameof [::arm ::com] [_ _]
  "armcom")

(defmethod nameof [::arm ::mex] [_ _]
  "armmex")

(defmethod nameof [::arm ::solar] [_ _]
  "armsolar")


(defmethod nameof [::core ::com] [_ _]
  "corcom")

(defmethod nameof [::core ::mex] [_ _]
  "cormex")

(defmethod nameof [::core ::solar] [_ _]
  "corsolar")
