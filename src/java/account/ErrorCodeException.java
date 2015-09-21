package account;

import org.springframework.transaction.TransactionException;

/**
 * 异常码错误异常类
 * 00 - 正常
 * ff - 系统未知错误
 * a1 - 交易重复  a2 - 参数错误
 * 01 - 账户不存在， 02 - 账户状态不正常， 03 - 账户余额不足， 04 - 账户不支持冻结
 */
public class ErrorCodeException extends TransactionException {
    String code;

    public String getCode() {
        return code;
    }

    public ErrorCodeException(String code) {
        super(code);
        this.code = code;
    }

    public ErrorCodeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCodeException(String code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }
}
