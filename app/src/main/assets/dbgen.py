import sqlite3

stopsdb = sqlite3.connect('stops.sqlite')
stoptimesdb = sqlite3.connect('stop_times.sqlite')
tripsdb = sqlite3.connect('trips.sqlite')
routesdb = sqlite3.connect('routes.sqlite')
stoproutesdb = sqlite3.connect('stop_routes.sqlite')

stoproutesdb.execute('''CREATE TABLE stop_routes
       (stop_id TEXT,
       route_id TEXT);''')

stop_cursor = stopsdb.execute("SELECT DISTINCT stop_id FROM stops")

for stop_row in stop_cursor:
    stop_id = stop_row[0]
    stoptimes_cursor = stoptimesdb.execute("SELECT DISTINCT trip_id FROM stop_times WHERE stop_id='"+stop_id+"'")
    for trips in stoptimes_cursor:
        trip_id = trips[0]
        trips_cursor = tripsdb.execute("SELECT DISTINCT route_id FROM trips where trip_id='"+trip_id+"'")
        for route in trips_cursor:
            route_id = route[0]
            stoproutesdb.execute("INSERT INTO stop_routes (stop_id,route_id) VALUES ('"+stop_id+"','"+route_id+"')")

stoproutesdb.commit()