(ns skynet.unit
  (:import
    (clojure.lang Keyword)
    (com.springrts.ai.oo.clb Unit UnitDef)))


(set! *warn-on-reflection* true)


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
  (def-name [this] this)
  nil
  (def-name [this] nil))


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
  ::lab)

(defmethod typeof "corlab" [_defname]
  ::lab)

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

; wtf are these
(defmethod typeof "armgplat" [_defname]
  ::gun-plat)

(defmethod typeof "corgplat" [_defname]
  ::gun-plat)

(defmethod typeof "armguard" [_defname]
  ::coastal)

(defmethod typeof "corpun" [_defname]
  ::coastal)

(defmethod typeof "armck" [_defname]
  ::cons-kbot)

(defmethod typeof "corck" [_defname]
  ::cons-kbot)

(defmethod typeof "armmakr" [_defname]
  ::converter)

(defmethod typeof "cormakr" [_defname]
  ::converter)

(defmethod typeof "armadvsol" [_defname]
  ::adv-solar)

(defmethod typeof "coradvsol" [_defname]
  ::adv-solar)

(defmethod typeof "armpw" [_defname]
  ::infantry)

(defmethod typeof "corak" [_defname]
  ::infantry)

(defmethod typeof "armham" [_defname]
  ::light-plasma)

(defmethod typeof "corthud" [_defname]
  ::light-plasma)

(defmethod typeof "armrock" [_defname]
  ::rocket)

(defmethod typeof "corstorm" [_defname]
  ::rocket)


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


(def cons-types
  #{::com ::cons-kbot})

(defn cons? [u]
  (contains? cons-types (typeof u)))

(def lab-types
  #{::lab ::factory})

(defn lab? [u]
  (contains? lab-types (typeof u)))


(defmulti builds (fn [unit] (typeof unit)))


(defmethod builds ::com [_unit]
  #{::mex ::solar ::lab ::radar ::llt})

(defmethod builds ::cons-kbot [_unit]
  #{::mex ::solar ::lab ::radar ::llt ::hlt ::coastal ::converter ::adv-solar})

(defmethod builds ::lab [_unit]
  #{::cons-kbot ::infantry ::light-plasma ::rocket})

(defmethod builds ::factory [_unit]
  #{::cons-vehicle})

(defmethod builds :default [_unit]
  #{})
