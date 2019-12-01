# torc
A simple BitTorrent client (work in progress)

Single-file torrent (https://wiki.theory.org/index.php/BitTorrentSpecification#Metainfo_File_Structure):

    d
        8:announce <num>:<URL>
        4:info d
            4:name <num>:<name>
            12:piece length i<num>e
            6:pieces <num>:<pieces (multiple of 20)>
            6:length i<num>e
            7:private i<1 or 0>e (OPTIONAL)
        e
        
        (OPTINAL fields underneath)
        13:announce-list l
            l 
                <num>:<URL> 
                ...
            e
        e
        13:creation date i<unix epoch time>e
        7:comment <num>:<random comment>
        10:created by <num>:<...>
        8:encoding <num>:<pieces encoding>
    e

Multi-file torrent:

    d
        8:announce <num>:<URL>
        4:info d
            4:name <num>:<name>
            12:piece length i<num>e
            6:pieces <num>:<pieces (multiple of 20)>
            5:files l
                d
                    6:length i<num>e
                    4:path l
                        <num>:<dir/path>
                        ...
                    e
                    6:md5sum <num>:<digest> (OPTIONAL)
                e
                ...
            e
            7:private i<1 or 0>e (OPTIONAL)
        e
        
        (OPTINAL fields underneath)
        13:announce-list l
            l 
                <num>:<URL> 
                ...
            e
        e
        13:creation date i<unix epoch time>e
        7:comment <num>:<random comment>
        10:created by <num>:<...>
        8:encoding <num>:<pieces encoding>
    e
    
Tracker Request (GET request) (https://wiki.theory.org/index.php/BitTorrentSpecification#Tracker_Request_Parameters):

    info_hash: SHA1 hash of the bencoded "info" dictionary.
    peer_id: ..
    port: ..
    uploaded: (bytes).
    downloaded: (bytes).
    left: bytes left to download before getting 100% of the torrent file(s).
    compact: 1 for "binary model response" and 0 for "dictionary model response" (see below).
    no_peer_id: ..
    event: "started", "stopped" or "completed".
    ip: address if thus host (OPTIONAL)
    numwant: number of peers this client wants (OPTIONAL)
    key: .. (OPTIONAL)
    trackerid: .. (OPTIONAL)
    
Tracker Response dictionary model (https://wiki.theory.org/index.php/BitTorrentSpecification#Tracker_Response):

    d
    	14:failure reason <num>:<string>
    	15:warning message <num>:<string> (OPTIONAL)
    	8:interval i<num>e
    	12:min interval i<num>e (OPTIONAL)
    	10:tracker id <num>:<string>
    	8:complete i<num>e		(seeders)
    	10:incomplete i<num>e	(leechers)
    	5:peers l
    		d
    			2:ip <num>:<string>		// IPv6 (hexed), IPv4 (dotted quad) or DNS name (string)
    			4:port i<num>e
    		e
    		...
    	e
    e
    
Tracker Response binary model:

    d
    	14:failure reason <num>:<string>
    	15:warning message <num>:<string> (OPTIONAL)
    	8:interval i<num>e
    	12:min interval i<num>e (OPTIONAL)
    	10:tracker id <num>:<string>
    	8:complete i<num>e		(seeders)
    	10:incomplete i<num>e	(leechers)
        5:peers <num>:<string> ... 	// <string> = 6 bytes (IPv4(4) + PORT(2))
    e