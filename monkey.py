from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
import commands
import sys
import time

apk_name = "com.vibin.billy"
print "start"

# connection to the current device, and return a MonkeyDevice object
device = MonkeyRunner.waitForConnection()

apk_path = device.shell('pm path '+apk_name)
if apk_path.startswith('package:'):
	print apk_name+" already installed."
else:
	print apk_name+" not installed, installing APK..."
	device.installPackage(apk_name)

print "launching "+apk_name+"..."
device.startActivity(component=apk_name+'/'+apk_name+'.MainActivity')

time.sleep(3)
for screen in range(4):
	for card in range(17):
		print "scrolling list"
		device.drag((400,700),(400,300),1,10)
		print "touching card"
		device.touch(500,500,MonkeyDevice.DOWN_AND_UP)
		time.sleep(3)
		print "pressing back"
		device.press('KEYCODE_BACK',MonkeyDevice.DOWN_AND_UP)
		time.sleep(2)
	print "scrolling viewpager"
	device.drag((1000,700),(0,700),1.25,1)
	time.sleep(4)

print "end of script"