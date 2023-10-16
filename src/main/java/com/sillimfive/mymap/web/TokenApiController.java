package com.sillimfive.mymap.web;

import com.sillimfive.mymap.service.TokenService;
import com.sillimfive.mymap.web.dto.Error;
import com.sillimfive.mymap.web.dto.*;
import com.sillimfive.mymap.web.dto.token.AuthenticationTokenRequest;
import com.sillimfive.mymap.web.dto.token.AuthenticationTokenResponse;
import com.sillimfive.mymap.web.dto.token.CreateAccessTokenRequest;
import com.sillimfive.mymap.web.dto.token.CreateAccessTokenResponse;
import com.sillimfive.mymap.web.dto.user.UserDto;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Tag(name = "Authentication", description = "token generation API")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth/token")
@RestController
public class TokenApiController {

    private final TokenService tokenService;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @PostMapping
    public ResponseEntity<AuthenticationTokenResponse> OauthAuthenticateToken(@RequestBody AuthenticationTokenRequest tokenRequest){

        AuthenticationTokenResponse authTokenResponse =
                tokenService.getAuthTokenResponse(tokenRequest.getAccessToken(), tokenRequest.getTokenType());

        return ResponseEntity.ok(authTokenResponse);
    }

    @Operation(summary = "액세스 토큰 갱신", description = "Refresh access token (desc)")
    @PostMapping("/renew")
    public ResultSet createNewAccessToken(
            @RequestBody CreateAccessTokenRequest request
    ){
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());

        Error error = Error.builder()
                .code(HttpStatus.CREATED)
                .message("토큰 발급 성공 ").build();

        return ResultSet.builder()
                .error(error)
                .data(new CreateAccessTokenResponse(newAccessToken))
                .result("SUCCESS")
                .build();
    }

    @Operation(summary = "토큰 발급받기",
                description = "<b>Receives tokens (accessToken, refreshToken) and userInfo</b><br>" +
                            "OAuth 인증을 통해 Authorization Server에서 받은 access 토큰과 토큰의 유형(구글/카카오)을 전달하여" +
                            "현 Application의 인증에 필요한 토큰 발급" +
                        "<br><br>" +
                        "todo: 관련 로직 미구현(spec만 명시)에 따라 로직 구현 필요")
//    @PostMapping
    public JSONObject authenticationToken(@RequestBody AuthRequestDto tokenInfo) throws IOException {
        JSONObject json = new JSONObject();

        String email = "mun1103.dev@gmail.com";

        //todo: @승환 관련로직 확인 및 추가 필!
        /*
         * successFullHandler 에 따라 생성함. 14, 1
         */

        Date now = new Date();
        Duration access = Duration.ofDays(1);
        Duration refresh = Duration.ofDays(14);

        String accessToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + access.toMillis()))
                .setSubject(email)
                .signWith(SignatureAlgorithm.HS256, "mymap")
                .compact();

        String refreshToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refresh.toMillis()))
                .setSubject(email)
                .signWith(SignatureAlgorithm.HS256, "mymap")
                .compact();

        UserDto userDto = new UserDto();
        userDto.setNickname("sample");
        userDto.setEmail(email);

        json.put("accessToken", accessToken);
        json.put("refreshToken", refreshToken);
        json.put("user", userDto);

        return json;
    }


    @GetMapping("/test/kakao")
    public String kakao() {
        String url = new StringBuilder("https://kauth.kakao.com/oauth/authorize?response_type=code")
                .append("&client_id=").append(clientId)
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)).toString();

        return url;
    }
}
