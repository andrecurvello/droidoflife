Droid of Life
=============

This is an experimental game for Android based on [Conway's Game of Life][1].

It is available on the Android Market for free: [Droid of Life][2]


Feel free to fork, create issues, [buy me a beer][3] or just having fun!

If you fork it, feel free to send me pull requests for your awesome new features so they'll make it on the market.

LICENSE
=======
 ******************************************************************************
	Droid of Life, an open source Android game based on Conway's Game of Life
	Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************

CONTRIBUTION
============
If you want to contribute to Droid of Life you'll need the [ActionBarSherlock][4]
libraries, the Android Development Kit and Eclipse ADT.

Integration ActionBarSherlock into Eclipse
------------------------------------------
Download version 4.x of [ActionBarSherlock][5] and unpack it to wherever it fits you. To integrate ActionBarSherlock into Eclipse you must add it as a "library project". Open Eclipse and create a new Android Project using the ./library path from the downloaded sources.

* File -> New -> Android Project -> Create project from existing source
* Point to the ActionBarSherlock ./library folder

If you didn't change the default name you should see a new project in the Package Explorer named "com_actionbarsherlock". If you haven't added the DroidOfLife App to Eclipse yet it's time to do so because we must tell the DroidOfLife Project to use the newly added libraries now.

* In the Package Explorer right-click on the DroidOfLife project and select "Properties".
* Select "Android" in the left pane and add the ActionBarSherlock library in the "Library" window.

That's it.

[1]: http://en.wikipedia.org/wiki/Conway%27s_Game_of_Life
[2]: https://market.android.com/details?id=com.chrulri.droidoflife
[3]: https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=chrulri@gmail.com&item_name=droid-of-life
[4]: http://actionbarsherlock.com/
[5]: http://actionbarsherlock.com/download.html
