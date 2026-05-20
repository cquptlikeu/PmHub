package com.laigeoffer.pmhub.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysRole;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.system.service.ISysMenuService;
import com.laigeoffer.pmhub.system.service.ISysPermissionService;
import com.laigeoffer.pmhub.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author canghe
 */
@Component
public class SysPermissionService implements ISysPermissionService {
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            roles.add("admin");
        } else {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     *
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            /**
             *   - 第一个 *：所有模块/服务
             *   - 第二个 *：所有控制器/方法
             *   - 第三个 *：所有操作（增删改查等）
             *   - *:*:* - 拥有所有模块、所有控制器、所有操作的权限
             */
            perms.add("*:*:*");
        } else {
            // 非管理员，根据角色分配权限
            List<SysRole> roles = user.getRoles();
            if (ObjectUtil.isNotEmpty(roles) && roles.size() > 1) {
                // 多角色：遍历每个角色，收集权限
                for (SysRole role : roles) {
                    //根据角色ID查询该角色拥有的所有菜单权限标识，返回一个权限字符串集合。
                    Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                    role.setPermissions(rolePerms);   // 给角色设置权限
                    perms.addAll(rolePerms);    // 合并所有角色的权限
                }
            } else {
                // 单角色：直接根据用户ID查询权限
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
