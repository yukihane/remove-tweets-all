package jp.himeji_cs.exception;

import lombok.NoArgsConstructor;

/**
 * 操作(削除)対象のツイートが存在しない場合の例外。
 */
@NoArgsConstructor
public class TargetNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -9075973044880216123L;

    public TargetNotFoundException(final String message) {
        super(message);
    }
}
