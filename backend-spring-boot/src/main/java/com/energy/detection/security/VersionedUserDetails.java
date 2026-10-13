package com.energy.detection.security;

/** Permite invalidar todos os tokens já emitidos após uma alteração sensível. */
public interface VersionedUserDetails {
    long getAuthVersion();
}
