package jp.himeji_cs.exception;

import lombok.NoArgsConstructor;

/**
 * 認可系の問題。未認証状態なども認可の問題とみなしています。
 */
@NoArgsConstructor
public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 7891149108388033192L;

    public AuthorizationException(final String message) {
        super(message);
    }
}
