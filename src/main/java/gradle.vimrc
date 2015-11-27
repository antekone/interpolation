setlocal errorformat=%E:compileJava%f:%l:\ %m,%E%f:%l:\ %m,%-Z%p^,%-C%.%#,%-G%.%#
set makeprg=gradle\ -q\ -p\ ../../../\ jar
