PREFIX_PATH=/data/data/com.rk.xededitor


# Override umask
umask 0022

export LD_LIBRARY_PATH=$PREFIX_PATH/root/lib
export LANG=C.UTF-8

ROOTFS=$PREFIX_PATH/rootfs
PATH=/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games
ARGS="$(cat $PREFIX_PATH/proot_args)"

$PREFIX_PATH/root/bin/proot \
  --bind=/apex \
  --bind=/system \
  --bind=/vendor \
  --bind=/sdcard \
  --bind=/storage \
  --bind=/dev \
  --bind=/proc \
  --bind=/sys \
  --bind="/data/data/com.rk.xededitor:/data/data/com.termux" \
  --bind="/dev/urandom:/dev/random" \
  --bind="/proc/self/fd:/dev/fd" \
  --bind="/proc/self/fd/0:/dev/stdin" \
  --bind="/proc/self/fd/1:/dev/stdout" \
  --bind="/proc/self/fd/2:/dev/stderr" \
  --bind=$PREFIX_PATH \
  --bind="$ROOTFS/proc/.vmstat:/proc/vmstat" \
  --bind="$ROOTFS/proc/.stat:/proc/stat" \
  --bind="$PREFIX_PATH:/karbon" \
  --root-id \
  --rootfs=$ROOTFS \
  $ARGS \
  sh /init.sh "$@"