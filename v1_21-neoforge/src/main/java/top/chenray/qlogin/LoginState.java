package top.chenray.qlogin;

/**
 * 玩家登录状态枚举
 */
public enum LoginState {
    /** 未登录（已注册但未登录） */
    LOGGED_OUT,
    /** 已登录 */
    LOGGED_IN,
    /** 未注册 */
    UNREGISTERED
}
