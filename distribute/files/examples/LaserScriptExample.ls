function rectangle(x, y, width, height)
{
	move(x, y);
	line(x+width, y);
	line(x+width, y+height);
	line(x, y+height);
	line(x, y);
}
for (var power = 0; power < 100; power += 10)
{
	set("power", power);
	for (var speed = 0; speed < 100; speed += 10)
	{
		set("speed", speed);
		rectangle(11*power/10.0, 11*speed/10.0, 10, 10);
	}
}

