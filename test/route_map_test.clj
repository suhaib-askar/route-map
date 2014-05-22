(ns route-map-test
  (:require [clojure.test :refer :all]
            [route-map :as rm]))
;; TODO
;; * url helper
;; * nested params (full naming or fallback to id)
;; * dsl
;; * meta-info
;; * handler
;; * [:*]
;; * [:param #"regexp"]

(deftest test-pathify
  (is (rm/pathify "/") [])
  (is (rm/pathify "/users/") ["users"])
  (is (rm/pathify "users/") ["users"])
  (is (rm/pathify "users") ["users"])
  (is (rm/pathify "users/1/show") ["users" "1" "show"]))

;; DSL example
(defn resource [nm & [chld]]
  "DSL example"
  {nm {:GET {:fn 'index}
       :POST {:fn 'create}
       [(keyword (str nm "_id"))]
       (merge
         {:GET {:fn 'show}
          :PUT {:fn 'update}
          :DELETE {:fn 'delete}} (or chld {})) }})


(rm/match [:get "/users/1/posts/2/comments/3"] routes-2)

(def meta-routes
  {:GET ^{:fls [1]} 'root })

(def GET :GET)
(def POST :POST)
(def PUT :PUT)

(def routes
  {GET    {:.desc "Root"}
   "posts" {:.roles   #{:author :admin}
            :.filters [:user-required]
            GET       {:.desc "List posts"}
            POST      {:.desc "Create post"}
            [:post-id] {GET       {:.desc "Show post"}
                        POST      {:.desc "Update post"}
                        "publish"  {POST {:.desc "Publish post"}}
                        "comments" {GET  {:.desc "comments"}
                                    [:comment-id] {GET {:.desc "show comment"}
                                                   PUT {:.desc "update comment"}}}}}
   "users" {:.roles   #{:admin}
            GET       {:.desc "List users"}
            POST      {:.desc "Create user" :.roles #{:admin}}
            "active"   {GET {:.desc "Filtering users"}}

            [:user-id] {GET       {:.desc "Show user"}
                        POST      {:.desc "Update user"}
                        "activate" {POST {:.desc "Activate"}}}}})
;; helper
#_(rm/path (url :.. "users" :user-id "activate" {:user-id 1})
           current-route routes)

(defn- get-desc [path]
  (:.desc
    (:match
      (rm/match path routes))))

(defn- get-params [path]
  (:params
    (rm/match path routes)))

(time
  (doseq [x (take 10000 (range))]
    (rm/match [:post (str "/users/" x "/activate")] routes)))

(deftest match-routes
  (is (= (rm/match [:get "some/unexistiong/route"] routes)
         nil))

  (is (= (get-desc [:get "users/active"]) "Filtering users"))
  (is (= (get-desc [:get "/"]) "Root"))
  (is (= (get-desc [:post "users/5/activate"]) "Activate"))

  (is (= (get-params [:post "users/5/activate"]) {:user-id "5"}))

  (is (=
       (count
         (:parents
           (rm/match [:post "users/5/activate"] routes)))
       4))
  (is (= (mapv :.filters (:parents (rm/match [:get "posts/1"] routes)))
         [nil [:user-required] nil]))
  )

(run-tests)
