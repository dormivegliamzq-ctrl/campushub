package com.campushub.common;

/**
 * 用户上下文：把当前登录用户暂存在 ThreadLocal 里，
 * 同一个请求内的任何代码（Controller/Service）都能直接取到"当前是谁"。
 */
public final class UserContext {

    private UserContext() {
    }

    /** 当前登录用户快照 */
    public record LoginUser(Long userId, String username) {
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        HOLDER.set(new LoginUser(userId, username));
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    /** 请求结束必须清理，否则线程池复用时会串号 */
    public static void clear() {
        HOLDER.remove();
    }
}
