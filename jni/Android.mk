#******************************************************************************
#*  Droid of Life, an open source Android game based on Conway's Game of Life *
#*  Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>                  *
#*                                                                            *
#*  This program is free software: you can redistribute it and/or modify      *
#*  it under the terms of the GNU General Public License as published by      *
#*  the Free Software Foundation, either version 3 of the License, or         *
#*  (at your option) any later version.                                       *
#*                                                                            *
#*  This program is distributed in the hope that it will be useful,           *
#*  but WITHOUT ANY WARRANTY; without even the implied warranty of            *
#*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
#*  GNU General Public License for more details.                              *
#*                                                                            *
#*  You should have received a copy of the GNU General Public License         *
#*  along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
#******************************************************************************
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := dol
LOCAL_SRC_FILES := dol.c
LOCAL_LDLIBS    := -llog -ljnigraphics
#LOCAL_CFLAGS    := -DDEBUG
include $(BUILD_SHARED_LIBRARY)
