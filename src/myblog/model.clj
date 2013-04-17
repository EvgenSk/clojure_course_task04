(ns myblog.model
  (:require [taoensso.carmine :as car]))

;; For Redis
(def pool         (car/make-conn-pool)) ; See docstring for additional options
(def spec-server1 (car/make-conn-spec :host "127.10.126.129" :port 15001)) ; ''
(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(def header-str "header")
(def content-str "content")

(defn create-article [item]
  (let [id (wcar (car/incr 'id-generator))
        header-key (str header-str ":" id)
        content-key (str content-str ":" id)]
    (wcar (car/set header-key (:header item))
          (car/set content-key (:content item)))))

(defn articles-vector 
  "Get hash-maps {:id :header :content} for ids-vector"
  ([ids] (articles-vector ids []))
  
  ([ids result] (if (empty? ids)
                 result
                 (let [id-str (first ids)
                       id-int (Integer/parseInt id-str)
                       header (str header-str ":" id-str)
                       content (str content-str ":" id-str)
                       article (wcar (car/get header)
                                     (car/get content))]
                   (articles-vector (rest ids) 
                                    (conj result {:id id-int :header (article 0) :content (article 1)}))))))

(defn select-article []
  (let [keys (wcar (car/keys (str header-str "*")))]
    (->> (map #(get (clojure.string/split % #":") 1) keys)
         (articles-vector))))

(defn find-article [id]
  (let [header-key (str header-str ":" id)
        content-key (str content-str ":" id)
        article (wcar (car/get header-key)
                      (car/get content-key))]
    {:id id :header (article 0) :content (article 1)}))

(defn update-article [item]
  (let [id (:id item)
        header-key (str header-str ":" id)
        content-key (str content-str ":" id)]
    (wcar (car/set header-key (:header item))
          (car/set content-key (:content item)))))

(defn delete-article [id]
  (let [keys (wcar (car/keys (str "*:" id)))]
    (wcar (car/del (keys 0))
          (car/del (keys 1)))))
