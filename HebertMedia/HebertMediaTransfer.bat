@ECHO OFF

SETLOCAL ENABLEDELAYEDEXPANSION

SET _cp=H:\groovyscripts\lib\sanselan-0.97-incubator.jar;H:\groovyscripts\lib\jna.jar;H:\groovyscripts\lib\subs4me.jar

groovy -cp "!_cp!" H:\groovyscripts\HebertMediaTransfer.groovy %1 %2 %3