package com.dodamdodam.backend.global.web;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;

public final class ClientAddressResolver {
    /** 상태 없이 공유하는 주소 변환 함수만 제공한다. */
    private ClientAddressResolver() { }

    /** Forwarded 헤더 wrapper의 주소 대신 서블릿 컨테이너가 받은 실제 연결 주소를 읽는다. */
    public static InetAddress resolve(HttpServletRequest request) {
        ServletRequest original = request;
        while (original instanceof ServletRequestWrapper wrapper) {
            original = wrapper.getRequest();
        }
        try {
            // 문자열 DNS 조회를 사용하지 않아 요청값으로 외부 이름을 조회하지 않는다.
            return InetAddress.ofLiteral(original.getRemoteAddr());
        }
        catch (IllegalArgumentException invalid) {
            throw new AuthStateUnavailableException();
        }
    }
}
