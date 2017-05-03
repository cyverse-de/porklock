(ns porklock.vault
  (:use [vault.client.http])
  (:require [vault.core :as vault])
  (:import [java.util Base64]))

(defn client
  "Creates and returns a Vault client that will connect to the provided Vault
   API URI and is already authenticated with the given token."
  [uri token]
  (-> (vault/new-client uri)
      (vault/authenticate! :token token)))

(defn- read-config
  "Reads the iRODS config from vault using cl as the Vault client. The uuid is
   used to construct the path to the config inside Vault. This should return the
   iRODS config as a base64 encoded string."
  [cl uuid]
  (:config (vault/read-secret cl (str "cubbyhole/" uuid))))

(defn- decode
  "Decodes the base64 encoded config string."
  [config]
  (String. (.decode (Base64/getDecoder) config)))

(defn irods-config
  "The full workflow for reading the irods-config from Vault. Creates a Vault
   client, reads the config, and base64 decodes it, returning the result."
  [uri token uuid]
  (-> (client uri token)
      (read-config uuid)
      (decode)))
