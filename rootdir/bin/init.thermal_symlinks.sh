#!/vendor/bin/sh

# Create thermal symlinks for thermal zones and cooling devices
for f in /sys/class/thermal/thermal_zone*
do
  tz_name=$(<"$f/type")
  ln -s $f /dev/thermal/tz-by-name/$tz_name
done

for f in /sys/class/thermal/cooling_device*
do
  cdev_name=$(<"$f/type")
  ln -s $f /dev/thermal/cdev-by-name/$cdev_name
done

# Create symlinks for custom thermal nodes
if [ -f /sys/class/power_supply/battery/capacity ]; then
  ln -s /sys/class/power_supply/battery/capacity /dev/thermal/tz-by-name/capacity/capacity
fi

setprop vendor.thermal.link_ready 1
