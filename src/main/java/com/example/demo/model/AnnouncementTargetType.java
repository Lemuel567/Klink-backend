package com.example.demo.model;

public enum AnnouncementTargetType {
    ALL,      // every active member in the church
    ROLES,    // members whose role is in targetRoles
    GROUPS,   // members belonging to any group in targetGroupIds
    MEMBERS,  // specific members listed in targetMemberIds
    CUSTOM    // union of targetRoles members + targetGroupIds members
}
