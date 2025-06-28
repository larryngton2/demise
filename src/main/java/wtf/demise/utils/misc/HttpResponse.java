package wtf.demise.utils.misc;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HttpResponse {
    private int code;
    private String content;

    public HttpResponse(int status, String content) {
        this.code = status;
        this.content = content;
    }

    public String toString() {
        return "[ code = " + code +
                " , content = " + content + " ]";
    }
}