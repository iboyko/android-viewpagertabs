ViewPagerTabs
=============

An implementation of the [swipey tabs][1] for the
ViewPager from the [Android Compatibility Library][2]

![ViewPagerTabs Screenshots][9]



Usage
=====

*For an example of how to use this, see [android-viewpagertabs-example][3]

  1. Including In Your Project

		This is an [Android library project][4], you can include it by [referencing it as a library project][5] in
		Eclipse or ant.
		
		This project depends on the `ViewPager` class which is available in the
		[Android Compatibility Library][6].

  2. Include com.astuetz.viewpagertabs.ViewPagerTabs in your view.

		<com.astuetz.viewpagertabs.ViewPagerTabs
		   android:id="@+id/tabs"
		   android:layout_width="fill_parent"
		   android:layout_height="wrap_content" />
            
     If you want to add a colored line below the tabs, use something like this:
     
		 <View
		    android:layout_width="fill_parent"
		    android:layout_height="2dip"
		    android:background="#FF91A438" />
	        
     And then your ViewPager:
     
	     <android.support.v4.view.ViewPager
	        android:id="@+id/pager"
	        android:layout_width="fill_parent"
	        android:layout_height="fill_parent" />

  3. In your Activity, bind the ViewPager to the ViewPagerTabs in the `onCreate` method.

		 // Set the pager with an adapter
		 ViewPager pager = (ViewPager) findViewById(R.id.pager);
		 pager.setAdapter(new ExampleAdapter(this));

         // Bind the ViewPager to ViewPagerTabs
         ViewPagerTabs tabs = (ViewPagerTabs) findViewById(R.id.tabs);
		 tabs.setViewPager(pager);

     *Note*: The adapter of the ViewPager has to implement `ViewPagerTabProvider`.



Developed By
============

 * Andreas Stütz - <andreas.stuetz@gmail.com>



License
=======

    Copyright 2011 Andreas Stütz

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.






 [1]: http://www.pushing-pixels.org/2011/08/11/android-tips-and-tricks-swipey-tabs.html
 [3]: http://www.google.com
 [4]: https://github.com/pakerfeldt/android-viewflow
 [4]: http://developer.android.com/guide/developing/projects/projects-eclipse.html
 [5]: http://developer.android.com/guide/developing/projects/projects-eclipse.html#ReferencingLibraryProject
 [6]: http://developer.android.com/sdk/compatibility-library.html
 [7]: http://www.google.com/intl/en_com/images/srpr/logo3w.png
