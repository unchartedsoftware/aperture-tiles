# Copyright (c) 2015 Uncharted Inc.
# http://www.uncharted.software/
#
# Released under the MIT License.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
# of the Software, and to permit persons to whom the Software is furnished to do
# so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

__author__ = 'Chris Bethune'

#!/usr/bin/env python
#!/usr/bin/python

import urllib2
import json
import sys
import pytz
from dateutil.parser import parse

components = {'a1 - build', 'a2 - maintenance', 'a3 - binning', 'a4 -tiling', 'a5 - server', 'a6 - client', 'a7 - examples', 'a8 - doc', 'a9 - test'}
types = {'t1 - bug', 't2 - feature', 't3 - refactor', 't4 - style', 't5 - question'}

prs_by_component = {}

# takes two args - start + end date for allowed PRs
# example format: 2014-05-01
start_date = pytz.utc.localize(parse(sys.argv[1]))
end_date = pytz.utc.localize(parse(sys.argv[2]))

# fetch the 100 most recent pulls
response = urllib2.urlopen('https://api.github.com/repos/unchartedsoftware/aperture-tiles/pulls?state=all&per_page=100')
pulls = json.loads(response.read())
response.close()

for pull in pulls:
    # Only process merged files
    merged_at_val = pull['merged_at']
    merge_date = None
    if merged_at_val is None:
        continue

    # Only process files merged between our supplied start and end dates
    merge_date = parse(merged_at_val)
    if start_date > merge_date or merge_date > end_date:
        continue

    # fetch the labels associated with the pull
    issue = pull['_links']['issue']['href']
    issue_response = urllib2.urlopen(issue)
    issue_data = json.loads(issue_response.read())
    issue_response.close()

    labels = issue_data['labels']
    # skip this entry when the label is NOLOG
    if 'NOLOG' in labels:
        continue

    # pull out type and component
    component = "other"
    change_type = "other"
    for label in labels:
        label_name = label['name'].lower()
        if label_name in components:
            component = label_name[5:]
        elif label_name in types:
            change_type = label_name[5:]

    # create an entry
    component_list = []
    if component in prs_by_component:
        component_list = prs_by_component[component]
    else:
        prs_by_component[component] = component_list

    component_list.append('#' + str(pull['number']) + ' ' + change_type + ': ' + pull['title'])

print '<Release Description Here>'
print '# Changes'
for component, pulls in prs_by_component.iteritems():
    print '\n'
    print '## ' + component
    for pull in pulls:
        print '* ' + pull
