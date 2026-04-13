package itheima.vo;

import lombok.Data;

/**
 * 用户
 */
@Data
public class UserInfo{

    private String username;
    private String password;
    private long phone;
    private int age;
}
