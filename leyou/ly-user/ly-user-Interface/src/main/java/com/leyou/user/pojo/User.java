package com.leyou.user.pojo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.Date;

@Data
@Table(name = "tb_user")
public class User {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;

    @NotEmpty(message = "用户名不能为空")
    @Length(min = 1, max = 10, message = "用户名只能在1~10位之间")
    private String username; //用户名

    @JsonIgnore
    @Length(min = 6, max = 30, message = "用户名只能在6~30位之间")
    private String password;    //密码

    @Pattern(regexp = "^1([38]\\d|5[0-35-9]|7[3678])\\d{8}$",message = "手机号不正确")
    private String phone;   //电话号码

    private Date created;   //创建时间

    @JsonIgnore
    private String salt; //密码的盐值
}
