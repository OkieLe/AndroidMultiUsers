package io.github.okiele.users;

interface ISingleUser {
    boolean get(int userId);
    boolean set(String key, String value);
}
